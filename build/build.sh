#!/bin/bash
echo "checking dependencies...";
missing_deps=false
for cmd in mvn svn fakeroot dpkg-deb alien makensis lintian rsync tar; do 
	if ! [ -x "$(command -v ${cmd})" ]; then
		echo "error: ${cmd} is not installed" >&2
		missing_deps=true
	fi
done
if [ "$missing_deps" = true ]; then
	echo "aborting..."
	exit 1
fi

cd ${0%/*}
cd ..
BUILD_TIME="2022-04-18 00:00:00"
export SOURCE_DATE_EPOCH=$(date -d "${BUILD_TIME}" +%s)
export DEB_BUILD_TIMESTAMP=${SOURCE_DATE_EPOCH}
ver=$(grep -oP '(?<=version>)[^<]+' pom.xml | head -n 1);
rev=$(svn info | grep Revision | cut -d ' ' -f 2 | sed 's/\s//g');
version=${ver}-${rev}
default_plugins="notifier-log notifier-mail notifier-notifyosd notifier-script notifier-sound sidebar-note"

echo "building $version (setting build time to $BUILD_TIME)";
rm -r src/main/resources/plugins dist
mvn -q clean install -Dmaven.test.skip=true
if (( $? )); then
  echo "build failed, aborting..."
  exit $?
fi

# build plugin
echo "building plugins..."
mkdir -p src/main/resources/plugins dist/plugins
for p in $(ls plugins); do
	echo "  building ${p}"
	cd plugins/${p}
	mvn -q clean package -Dmaven.test.skip=true
	if (( $? )); then
        echo "plugin build failed: ${p}" >&2
	    #cd ../..
        #exit $?
    else
        if [ -f target/*-jar-with-dependencies.jar ]; then
            cp target/*-jar-with-dependencies.jar ../../dist/plugins/${p}.jar
        else
            cp target/*.jar ../../dist/plugins/${p}.jar
        fi
        is_default=$(echo ${default_plugins} | grep "${p}" | wc -l)
        if ((${is_default} > 0)); then
            cp ../../dist/plugins/${p}.jar ../../src/main/resources/plugins
        fi
    fi
	cd ../..
done

# build jar package
echo "building jar..."
mvn -q package -Dmaven.test.skip=true
if (( $? )); then
  echo "packaging failed, aborting..."
  exit $?
fi
cp target/jnetmap-${ver}-jar-with-dependencies.jar dist/jNetMap-${version}.jar

cd build
# build deb package
echo "building deb..."
mkdir tmp
rsync -rC DEBIAN tmp/
rsync -rC usr tmp/
cp ../dist/jNetMap-${version}.jar tmp/usr/share/jnetmap/jNetMap.jar
sed -i "s/jNetMap X/jNetMap $ver/" tmp/usr/share/man/man1/jnetmap.1 # set version
sed -i "s/DATE/$(date "+%B %Y")/" tmp/usr/share/man/man1/jnetmap.1 # set date
sed -i "s/Version=.*/Version=$ver/" tmp/usr/share/applications/jnetmap.desktop # set version
sed -i "s/Version: .*/Version: $ver/" tmp/DEBIAN/control # set version
sed -i "s/Installed-Size: .*/Installed-Size: `du -s tmp/usr | awk '{print $1}'`/" tmp/DEBIAN/control # set size
gzip -nf9 tmp/usr/share/doc/jnetmap/changelog
gzip -nf9 tmp/usr/share/man/man1/jnetmap.1
find tmp/ -type d -exec chmod 0755 {} \;
find tmp/ -type f -exec chmod 0644 {} \;
find tmp/ -type f -name '*.sh' -exec chmod 0755 {} \;
find tmp/ -exec touch --date=@${SOURCE_DATE_EPOCH} {} \;
chmod 0755 tmp/usr/local/bin/jnetmap tmp/DEBIAN/p*
fakeroot dpkg-deb -b ./tmp jnetmap-${version}_all.deb
mv jnetmap-${version}_all.deb ../dist/
rm -rf tmp

# build rpm package
echo "building rpm..."
mkdir tmp
cp ../dist/jnetmap-${version}_all.deb tmp/
fakeroot alien --to-rpm -k tmp/jnetmap-${version}_all.deb
mv *.rpm ../dist/jnetmap-${version}_all.rpm
rm -rf tmp

# build makefile tar.gz
echo "building tar.gz..."
mkdir jNetMap-${version}
cd jNetMap-${version}
cp ../Makefile ./
cp ../../dist/jNetMap-${version}.jar ./jNetMap.jar
cp ../usr/local/bin/jnetmap ./
cp ../usr/share/applications/jnetmap.desktop ./
cp ../usr/share/man/man1/jnetmap.1 ./
gzip -nf9 jnetmap.1
cp -R ../usr/share/jnetmap/* ./
cd ..
#find jNetMap-${version}/ -exec touch --date=@${SOURCE_DATE_EPOCH} {} \;
GZIP=-n tar --mtime="${BUILD_TIME}" -pczf ../dist/jNetMap-${version}.tar.gz jNetMap-${version}
rm -rf jNetMap-${version}

# build Windows Installer
echo "building exe..."
mkdir tmp
rsync -rC exe/ tmp/
cp ../dist/jNetMap-${version}.jar tmp/jNetMap.jar
cd tmp/
sed -i "s/VIProductVersion .*/VIProductVersion \"$ver.0\"/" jNetMap.nsi # set version
sed -i "s/FileVersion .*/FileVersion $rev/" jNetMap.nsi # set revision
makensis -V2 jNetMap.nsi
mv jNetMap.exe ../../dist/jNetMap-${version}-setup.exe
cd ../
rm -rf tmp

# build OS X app (reportedly broken, can't test)
# echo "building app..."
# mkdir tmp
# rsync -rC jNetMap.app tmp/
# cp ../dist/dist/jNetMap-${version}.jar ./tmp/jNetMap.app/Contents/MacOS/application.jar
# cd tmp/
# zip -r -q ../../dist/jNetMap-${version}-OSX.zip jNetMap.app
# cd ../
# rm -rf tmp

# create javadoc
# echo "building javadoc..."
# mvn -q javadoc:jar

# clean up
rm -r ~/rpmbuild
find ../dist -exec touch --date=@${SOURCE_DATE_EPOCH} {} \;
echo "running lintian..."
lintian ../dist/jnetmap-${version}_all.deb

# update dev builds
dev_dest=/media/DSweb/jnetmap
if [ -d $dev_dest ]; then
    echo "updating dev downloads...";
    rsync -ru --delete ../dist/ $dev_dest
    tree -H '.' -L 2 -T "jNetMap dev builds" --noreport --charset utf-8 $dev_dest | sed -e "s/\.VERSION {/.VERSION {opacity: 0.2;/" > $dev_dest/index.html
fi

echo "done building $version"
